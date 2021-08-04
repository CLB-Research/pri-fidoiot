// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;

/**
 * Device initialization Database manager.
 */
public class DiDbManager {

  private static final LoggerService logger = new LoggerService(DiDbManager.class);

  /**
   * Create DI Tables.
   *
   * @param dataSource The datasource to use.
   */
  public void createTables(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {

      String sql = "CREATE TABLE IF NOT EXISTS "
          + "MT_SETTINGS ("
          + "ID INT NOT NULL, "
          + "CERTIFICATE_VALIDITY_DAYS INT, "
          + "AUTO_ASSIGN_CUSTOMER_ID INT NULL DEFAULT NULL, "
          + "RENDEZVOUS_INFO VARCHAR(4096), "
          + "UNIQUE (ID)"
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "MT_CUSTOMERS ("
          + "CUSTOMER_ID INT NOT NULL, "
          + "NAME VARCHAR(255), "
          + "KEYS VARCHAR(4096), "
          + "UNIQUE (CUSTOMER_ID)"
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "MT_DEVICES("
          + "GUID CHAR(36), "
          + "SERIAL_NO VARCHAR(255) PRIMARY KEY, "
          + "VOUCHER BLOB, "
          + "CUSTOMER_ID INT NULL DEFAULT NULL, "
          + "M_STRING BLOB, "
          + "STARTED TIMESTAMP, "
          + "COMPLETED TIMESTAMP NULL DEFAULT NULL, "
          + "PRIMARY KEY ( SERIAL_NO), "
          + "FOREIGN KEY (CUSTOMER_ID) REFERENCES MT_CUSTOMERS(CUSTOMER_ID), "
          + "UNIQUE ( SERIAL_NO), "
          + "UNIQUE ( GUID)"
          + ");";

      stmt.executeUpdate(sql);

      sql = "SELECT COUNT(ID) FROM MT_SETTINGS";

      try (ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) {
          if (rs.getInt(1) == 0) {
            sql = "INSERT INTO MT_SETTINGS ("
                + "ID,"
                + "CERTIFICATE_VALIDITY_DAYS,"
                + "RENDEZVOUS_INFO) "
                + "VALUES (1,'3650',"
                + "'81858205696c6f63616c686f73748203191f68820c018202447f0000018204191f69');";
            stmt.executeUpdate(sql);
          }

        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the auto enroll customer.
   *
   * @param ds The datasource to use.
   * @param customerId The id of the customer.
   */
  public void setAutoEnroll(DataSource ds, int customerId) {
    String sql = "UPDATE MT_SETTINGS "
        + "SET AUTO_ASSIGN_CUSTOMER_ID = ? "
        + "WHERE ID = 1";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, customerId);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Ads a customer to the Customers table.
   *
   * @param ds   The datasource to use.
   * @param id   The id of the customer.
   * @param name The name of the customer.
   * @param keys A PEM string containing public keys.
   */
  public void addCustomer(DataSource ds, int id, String name, String keys) {

    String sql = ""
        + "MERGE INTO MT_CUSTOMERS   "
        + "KEY (CUSTOMER_ID) "
        + "VALUES (?,?,?); ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, id);
      pstmt.setString(2, name);
      pstmt.setString(3, keys);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Assign customer ID to voucher having the input GUID.
   * 
   * @param ds The datasource to use.
   * @param id The id of the customer.
   * @param serial The Serial Number of the device
   */
  public int assignCustomerToVoucher(DataSource ds, int id, String serial) {

    String sql = ""
        + "UPDATE MT_DEVICES   "
        + "SET CUSTOMER_ID=? "
        + "WHERE SERIAL_NO=?; ";

    int rowsAffected = 0;
    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, id);
      pstmt.setString(2, serial);
      rowsAffected = pstmt.executeUpdate();
    } catch (SQLException e) {
      logger.error("Unable to update customer for serial no: " + serial);
      throw new RuntimeException(e);
    }
    return rowsAffected;
  }

  /**
   * Assign customer ID to voucher having the input GUID.
   *
   * @param ds The datasource to use.
   * @param rvInfo The GUID of the device
   */
  public void addRvInfo(DataSource ds, String rvInfo) {

    String sql = "" + "UPDATE MT_SETTINGS   " + "SET RENDEZVOUS_INFO=? " + "WHERE ID=1; ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, rvInfo);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
