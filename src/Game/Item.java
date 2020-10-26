/**
 * rscplus
 *
 * <p>This file is part of rscplus.
 *
 * <p>rscplus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>rscplus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with rscplus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * <p>Authors: see <https://github.com/RSCPlus/rscplus>
 */
package Game;

import Client.Logger;
import Client.Settings;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;

/**
 * This class defines items and provides a static method to patch item names as needed according to
 * {@link Settings#NAME_PATCH_TYPE}.
 */
public class Item {

  public static String[] item_name;
  public static String[] item_commands;

  public int x;
  public int y;
  public int width;
  public int height;
  public int id;

  public Item(int x, int y, int width, int height, int id) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.id = id;
  }

  /** Patches item names as specified by {@link Settings#NAME_PATCH_TYPE}. */
  public static void patchItemNames() {
    int namePatchType = Settings.NAME_PATCH_TYPE.get(Settings.currentProfile);
    Connection c = null;

    try {
      Class.forName("org.sqlite.JDBC");

      // Check if running from a jar so you know where to look for the database
      if (new File("assets/itempatch.db").exists()) {
        c = DriverManager.getConnection("jdbc:sqlite:assets/itempatch.db");
      } else {
        c = DriverManager.getConnection("jdbc:sqlite::resource:assets/itempatch.db");
      }

      c.setAutoCommit(false);
      Logger.Info("Opened item name database successfully");

      switch (namePatchType) {
        case 1:
          queryDatabaseAndPatchItem(
              c, "SELECT item_id, patched_name FROM patched_names_type" + namePatchType + ";");
          break;
        case 2:
          queryDatabaseAndPatchItem(
              c, "SELECT item_id, patched_name FROM patched_names_type" + namePatchType + ";");
          queryDatabaseAndPatchItem(
              c,
              "SELECT item_id, patched_name FROM patched_names_type1 WHERE patched_names_type1.item_id NOT IN (SELECT item_id FROM patched_names_type2);");
          break;
        case 3:
          queryDatabaseAndPatchItem(
              c, "SELECT item_id, patched_name FROM patched_names_type" + namePatchType + ";");
          queryDatabaseAndPatchItem(
              c,
              "SELECT item_id, patched_name FROM patched_names_type1 WHERE patched_names_type1.item_id NOT IN (SELECT item_id FROM patched_names_type2) AND patched_names_type1.item_id NOT IN (SELECT item_id FROM patched_names_type3);");
          queryDatabaseAndPatchItem(
              c,
              "SELECT item_id, patched_name FROM patched_names_type2 WHERE patched_names_type2.item_id NOT IN (SELECT item_id FROM patched_names_type3);");
          break;
        case 0:
        default:
          break;
      }
      c.close();

    } catch (SQLTimeoutException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Queries an opened database and patches the items and names it returns.
   *
   * @param c the connection with a specific database
   * @param query a SQLite query statement
   */
  public static void queryDatabaseAndPatchItem(Connection c, String query) {
    try {
      Statement stmt = null;

      stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(query);

      while (rs.next()) {
        int itemID = rs.getInt("item_id");
        String patchedName = rs.getString("patched_name");
        item_name[itemID] = patchedName;
      }

      rs.close();
      stmt.close();

    } catch (SQLException e) {
      Logger.Error("Error patching item names from database values.");
      e.printStackTrace();
    }
  }

  /**
   * Patches discontinued edible item commands specified by {@link Settings#COMMAND_PATCH_TYPE}.
   * Removes completely the option to eat/drink
   */
  public static void patchItemCommands() {
    int commandPatchType = Settings.COMMAND_PATCH_TYPE.get(Settings.currentProfile);
    // ids of Half full wine jug, Disk of Returning, Pumpkin, Easter egg
    int[] edible_rare_item_ids = {246, 387, 422, 677};

    if (commandPatchType == 1 || commandPatchType == 3) {
      for (int i : edible_rare_item_ids) {
        item_commands[i] = "";
      }
    }
  }

  /**
   * Patches quest only edible item commands specified by Settings.COMMANDS_PATCH_TYPE. Swaps around
   * the option to eat/drink
   */
  public static boolean shouldPatch(int index) {
    if (Settings.SPEEDRUNNER_MODE_ACTIVE.get(Settings.currentProfile)) return false;

    int commandPatchType = Settings.COMMAND_PATCH_TYPE.get(Settings.currentProfile);
    // ids of giant Carp, chocolaty milk, Rock cake, nightshade
    int[] edible_quest_item_ids = {718, 770, 1061, 1086};
    boolean found = false;
    if (commandPatchType == 2 || commandPatchType == 3) {
      for (int i : edible_quest_item_ids) {
        if (index == i) {
          found = true;
          break;
        }
      }
      return found;
    } else {
      return false;
    }
  }

  public String getName() {
    return item_name[id];
  }

  // need to override this for Collections.frequency over in Renderer.java -> SHOW_ITEMINFO to count
  // duplicate-looking
  // items on ground correctly. without this, I believe it checks if location in memory is the same
  // for both objects.
  @Override
  public boolean equals(Object b) {
    if (b != null) {
      if (b.getClass() == this.getClass()) {
        Item bItem = (Item) b;
        return this.x == bItem.x && this.y == bItem.y && this.id == bItem.id;
      } else {
        return false;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    // This is an acceptable hash since it's fine if two unequal objects have the same hash
    // according to docs
    return this.x + this.y + this.width + this.height + this.id;
  }
}
