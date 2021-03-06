package mil.nga.geopackage.test;

import java.sql.SQLException;

import org.junit.Test;

/**
 * Test GeoPackage from an imported database
 * 
 * @author osbornb
 */
public class GeoPackageImportTest extends ImportGeoPackageTestCase {

	/**
	 * Constructor
	 */
	public GeoPackageImportTest() {

	}

	/**
	 * Test create feature table with metadata
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCreateFeatureTableWithMetadata() throws SQLException {

		GeoPackageTestUtils.testCreateFeatureTableWithMetadata(geoPackage);

	}

	/**
	 * Test create feature table with metadata and id column
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCreateFeatureTableWithMetadataIdColumn()
			throws SQLException {

		GeoPackageTestUtils
				.testCreateFeatureTableWithMetadataIdColumn(geoPackage);

	}

	/**
	 * Test create feature table with metadata and additional columns
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCreateFeatureTableWithMetadataAdditionalColumns()
			throws SQLException {

		GeoPackageTestUtils
				.testCreateFeatureTableWithMetadataAdditionalColumns(geoPackage);

	}

	/**
	 * Test create feature table with metadata, id column, and additional
	 * columns
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCreateFeatureTableWithMetadataIdColumnAdditionalColumns()
			throws SQLException {

		GeoPackageTestUtils
				.testCreateFeatureTableWithMetadataIdColumnAdditionalColumns(geoPackage);

	}

	/**
	 * Test delete tables
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testDeleteTables() throws SQLException {

		GeoPackageTestUtils.testDeleteTables(geoPackage);

	}
	
}
