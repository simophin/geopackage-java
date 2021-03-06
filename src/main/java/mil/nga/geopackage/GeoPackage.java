package mil.nga.geopackage;

import java.sql.ResultSet;

import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSet;
import mil.nga.geopackage.tiles.user.TileDao;

/**
 * GeoPackage database connection
 * 
 * @author osbornb
 */
public interface GeoPackage extends GeoPackageCore {

	/**
	 * Get a Feature DAO from Geometry Columns
	 *
	 * @param geometryColumns
	 * @return
	 */
	public FeatureDao getFeatureDao(GeometryColumns geometryColumns);

	/**
	 * Get a Feature DAO from Contents
	 *
	 * @param contents
	 * @return
	 */
	public FeatureDao getFeatureDao(Contents contents);

	/**
	 * Get a Feature DAO from a table name
	 *
	 * @param tableName
	 * @return
	 */
	public FeatureDao getFeatureDao(String tableName);

	/**
	 * Get a Tile DAO from Tile Matrix Set
	 *
	 * @param tileMatrixSet
	 * @return
	 */
	public TileDao getTileDao(TileMatrixSet tileMatrixSet);

	/**
	 * Get a Tile DAO from Contents
	 *
	 * @param contents
	 * @return
	 */
	public TileDao getTileDao(Contents contents);

	/**
	 * Get a Tile DAO from a table name
	 *
	 * @param tableName
	 * @return
	 */
	public TileDao getTileDao(String tableName);

	/**
	 * Perform a query on the database
	 *
	 * @param sql
	 * @param args
	 * @return cursor
	 * @since 1.1.2
	 */
	public ResultSet query(String sql, String[] args);

	/**
	 * Perform a foreign key check on the database
	 *
	 * @return null if check passed, open cursor with results if failed
	 * @since 1.1.2
	 */
	public ResultSet foreignKeyCheck();

	/**
	 * Perform an integrity check on the database
	 *
	 * @return null if check passed, open cursor with results if failed
	 * @since 1.1.2
	 */
	public ResultSet integrityCheck();

	/**
	 * Perform a quick integrity check on the database
	 *
	 * @return null if check passed, open cursor with results if failed
	 * @since 1.1.2
	 */
	public ResultSet quickCheck();

}
