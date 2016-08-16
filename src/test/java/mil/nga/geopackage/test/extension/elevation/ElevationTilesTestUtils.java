package mil.nga.geopackage.test.extension.elevation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.contents.ContentsDataType;
import mil.nga.geopackage.core.srs.SpatialReferenceSystem;
import mil.nga.geopackage.core.srs.SpatialReferenceSystemDao;
import mil.nga.geopackage.extension.ExtensionScopeType;
import mil.nga.geopackage.extension.Extensions;
import mil.nga.geopackage.extension.ExtensionsDao;
import mil.nga.geopackage.extension.elevation.ElevationTileResults;
import mil.nga.geopackage.extension.elevation.ElevationTiles;
import mil.nga.geopackage.extension.elevation.ElevationTilesAlgorithm;
import mil.nga.geopackage.extension.elevation.ElevationTilesCore;
import mil.nga.geopackage.extension.elevation.GriddedCoverage;
import mil.nga.geopackage.extension.elevation.GriddedCoverageDataType;
import mil.nga.geopackage.extension.elevation.GriddedTile;
import mil.nga.geopackage.projection.Projection;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.geopackage.projection.ProjectionTransform;
import mil.nga.geopackage.test.CreateElevationTilesGeoPackageTestCase.ElevationTileValues;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.matrix.TileMatrix;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSet;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSetDao;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileResultSet;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.geopackage.tiles.user.TileTable;

/**
 * Elevation Tiles test utils
 * 
 * @author osbornb
 */
public class ElevationTilesTestUtils {

	/**
	 * Test elevations GeoPackage
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param elevationTileValues
	 *            elevation tile values
	 * @param algorithm
	 *            algorithm
	 * @param allowNulls
	 *            true if nulls are allowed
	 * @throws Exception
	 */
	public static void testElevations(GeoPackage geoPackage,
			ElevationTileValues elevationTileValues,
			ElevationTilesAlgorithm algorithm, boolean allowNulls)
			throws Exception {

		// Verify the elevation shows up as an elevation table and not a tile
		// table
		List<String> tilesTables = geoPackage.getTileTables();
		List<String> elevationTables = ElevationTiles.getTables(geoPackage);
		TestCase.assertFalse(elevationTables.isEmpty());
		for (String tilesTable : tilesTables) {
			TestCase.assertFalse(elevationTables.contains(tilesTable));
		}

		TileMatrixSetDao dao = geoPackage.getTileMatrixSetDao();
		TestCase.assertTrue(dao.isTableExists());

		for (String elevationTable : elevationTables) {

			TileMatrixSet tileMatrixSet = dao.queryForId(elevationTable);
			TestCase.assertNotNull(tileMatrixSet);

			// Test the tile matrix set
			TestCase.assertNotNull(tileMatrixSet.getTableName());
			TestCase.assertNotNull(tileMatrixSet.getId());
			TestCase.assertNotNull(tileMatrixSet.getSrsId());
			TestCase.assertNotNull(tileMatrixSet.getMinX());
			TestCase.assertNotNull(tileMatrixSet.getMinY());
			TestCase.assertNotNull(tileMatrixSet.getMaxX());
			TestCase.assertNotNull(tileMatrixSet.getMaxY());

			// Test the tile matrix set SRS
			SpatialReferenceSystem srs = tileMatrixSet.getSrs();
			TestCase.assertNotNull(srs);
			TestCase.assertNotNull(srs.getSrsName());
			TestCase.assertNotNull(srs.getSrsId());
			TestCase.assertTrue(srs.getOrganization().equalsIgnoreCase("epsg"));
			TestCase.assertNotNull(srs.getOrganizationCoordsysId());
			TestCase.assertNotNull(srs.getDefinition());

			// Test the contents
			Contents contents = tileMatrixSet.getContents();
			TestCase.assertNotNull(contents);
			TestCase.assertEquals(tileMatrixSet.getTableName(),
					contents.getTableName());
			TestCase.assertEquals(ContentsDataType.ELEVATION_TILES,
					contents.getDataType());
			TestCase.assertEquals(ContentsDataType.ELEVATION_TILES.getName(),
					contents.getDataTypeString());
			TestCase.assertNotNull(contents.getLastChange());

			// Test the contents SRS
			SpatialReferenceSystem contentsSrs = contents.getSrs();
			TestCase.assertNotNull(contentsSrs);
			TestCase.assertNotNull(contentsSrs.getSrsName());
			TestCase.assertNotNull(contentsSrs.getSrsId());
			TestCase.assertNotNull(contentsSrs.getOrganization());
			TestCase.assertNotNull(contentsSrs.getOrganizationCoordsysId());
			TestCase.assertNotNull(contentsSrs.getDefinition());

			// Test the elevation tiles extension is on
			TileDao tileDao = geoPackage.getTileDao(tileMatrixSet);
			ElevationTiles elevationTiles = new ElevationTiles(geoPackage,
					tileDao);
			TestCase.assertTrue(elevationTiles.has());
			elevationTiles.setAlgorithm(algorithm);

			// Test the 3 extension rows
			ExtensionsDao extensionsDao = geoPackage.getExtensionsDao();

			Extensions griddedCoverageExtension = extensionsDao
					.queryByExtension(ElevationTilesCore.EXTENSION_NAME,
							GriddedCoverage.TABLE_NAME, null);
			TestCase.assertNotNull(griddedCoverageExtension);
			TestCase.assertEquals(GriddedCoverage.TABLE_NAME,
					griddedCoverageExtension.getTableName());
			TestCase.assertNull(griddedCoverageExtension.getColumnName());
			TestCase.assertEquals(ElevationTilesCore.EXTENSION_NAME,
					griddedCoverageExtension.getExtensionName());
			TestCase.assertEquals(ElevationTilesCore.EXTENSION_DEFINITION,
					griddedCoverageExtension.getDefinition());
			TestCase.assertEquals(ExtensionScopeType.READ_WRITE,
					griddedCoverageExtension.getScope());

			Extensions griddedTileExtension = extensionsDao.queryByExtension(
					ElevationTilesCore.EXTENSION_NAME, GriddedTile.TABLE_NAME,
					null);
			TestCase.assertNotNull(griddedTileExtension);
			TestCase.assertEquals(GriddedTile.TABLE_NAME,
					griddedTileExtension.getTableName());
			TestCase.assertNull(griddedTileExtension.getColumnName());
			TestCase.assertEquals(ElevationTilesCore.EXTENSION_NAME,
					griddedTileExtension.getExtensionName());
			TestCase.assertEquals(ElevationTilesCore.EXTENSION_DEFINITION,
					griddedTileExtension.getDefinition());
			TestCase.assertEquals(ExtensionScopeType.READ_WRITE,
					griddedTileExtension.getScope());

			Extensions tileTableExtension = extensionsDao.queryByExtension(
					ElevationTilesCore.EXTENSION_NAME,
					tileMatrixSet.getTableName(), TileTable.COLUMN_TILE_DATA);
			TestCase.assertNotNull(tileTableExtension);
			TestCase.assertEquals(tileMatrixSet.getTableName(),
					tileTableExtension.getTableName());
			TestCase.assertEquals(TileTable.COLUMN_TILE_DATA,
					tileTableExtension.getColumnName());
			TestCase.assertEquals(ElevationTilesCore.EXTENSION_NAME,
					tileTableExtension.getExtensionName());
			TestCase.assertEquals(ElevationTilesCore.EXTENSION_DEFINITION,
					tileTableExtension.getDefinition());
			TestCase.assertEquals(ExtensionScopeType.READ_WRITE,
					tileTableExtension.getScope());

			// Test the Gridded Coverage
			List<GriddedCoverage> griddedCoverages = elevationTiles
					.getGriddedCoverage();
			TestCase.assertNotNull(griddedCoverages);
			TestCase.assertFalse(griddedCoverages.isEmpty());
			for (GriddedCoverage griddedCoverage : griddedCoverages) {
				TestCase.assertTrue(griddedCoverage.getId() >= 0);
				TestCase.assertNotNull(griddedCoverage.getTileMatrixSet());
				TestCase.assertEquals(tileMatrixSet.getTableName(),
						griddedCoverage.getTileMatrixSetName());
				TestCase.assertEquals(GriddedCoverageDataType.INTEGER,
						griddedCoverage.getDataType());
				TestCase.assertTrue(griddedCoverage.getScale() >= 0);
				TestCase.assertTrue(griddedCoverage.getOffset() >= 0);
				TestCase.assertTrue(griddedCoverage.getPrecision() >= 0);
				griddedCoverage.getDataNull();
				griddedCoverage.getDataMissing();
			}

			// Test the Gridded Tile
			List<GriddedTile> griddedTiles = elevationTiles.getGriddedTile();
			TestCase.assertNotNull(griddedTiles);
			TestCase.assertFalse(griddedTiles.isEmpty());
			for (GriddedTile griddedTile : griddedTiles) {
				TileRow tileRow = tileDao.queryForIdRow(griddedTile
						.getTableId());
				testTileRow(geoPackage, elevationTileValues, elevationTiles,
						tileMatrixSet, griddedTile, tileRow, algorithm);
			}

			TileResultSet tileResultSet = tileDao.queryForAll();
			TestCase.assertNotNull(tileResultSet);
			TestCase.assertTrue(tileResultSet.getCount() > 0);
			while (tileResultSet.moveToNext()) {
				TileRow tileRow = tileResultSet.getRow();
				GriddedTile griddedTile = elevationTiles.getGriddedTile(tileRow
						.getId());
				testTileRow(geoPackage, elevationTileValues, elevationTiles,
						tileMatrixSet, griddedTile, tileRow, algorithm);
			}
			tileResultSet.close();

			// Perform elevation query tests
			testElevationQueries(geoPackage, elevationTiles, tileMatrixSet,
					algorithm, allowNulls);
		}

	}

	/**
	 * Perform tests on the tile row
	 * 
	 * @param geoPackage
	 * @param elevationTileValues
	 * @param elevationTiles
	 * @param tileMatrixSet
	 * @param griddedTile
	 * @param tileRow
	 * @param algorithm
	 * @throws IOException
	 * @throws SQLException
	 */
	private static void testTileRow(GeoPackage geoPackage,
			ElevationTileValues elevationTileValues,
			ElevationTiles elevationTiles, TileMatrixSet tileMatrixSet,
			GriddedTile griddedTile, TileRow tileRow,
			ElevationTilesAlgorithm algorithm) throws IOException, SQLException {

		TestCase.assertNotNull(griddedTile);
		TestCase.assertTrue(griddedTile.getId() >= 0);
		TestCase.assertNotNull(griddedTile.getContents());
		TestCase.assertEquals(tileMatrixSet.getTableName(),
				griddedTile.getTableName());
		long tableId = griddedTile.getTableId();
		TestCase.assertTrue(tableId >= 0);
		TestCase.assertTrue(griddedTile.getScale() >= 0);
		TestCase.assertTrue(griddedTile.getOffset() >= 0);
		griddedTile.getMin();
		griddedTile.getMax();
		griddedTile.getMean();
		griddedTile.getStandardDeviation();
		TestCase.assertNotNull(tileRow);

		TestCase.assertNotNull(tileRow);
		byte[] tileData = tileRow.getTileData();
		TestCase.assertTrue(tileData.length > 0);
		BufferedImage image = tileRow.getTileDataImage();

		// Get all the pixel values of the image
		short[] pixelValues = elevationTiles.getPixelValues(image);
		if (elevationTileValues != null) {
			for (int i = 0; i < pixelValues.length; i++) {
				TestCase.assertEquals(elevationTileValues.tilePixelsFlat[i],
						pixelValues[i]);
			}
		}

		int width = image.getWidth();
		int height = image.getHeight();

		// Get each individual image pixel value
		List<Short> pixelValuesList = new ArrayList<>();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				short pixelValue = elevationTiles.getPixelValue(image, x, y);
				pixelValuesList.add(pixelValue);

				// Test getting the pixel value from the pixel values
				// array
				short pixelValue2 = elevationTiles.getPixelValue(pixelValues,
						width, x, y);
				TestCase.assertEquals(pixelValue, pixelValue2);

				// Test getting the elevation value
				Double elevationValue = elevationTiles.getElevationValue(
						griddedTile, pixelValue);
				GriddedCoverage griddedCoverage = elevationTiles
						.getGriddedCoverage().get(0);
				int unsignedPixelValue = elevationTiles
						.getUnsignedPixelValue(pixelValue);
				if (elevationTileValues != null) {
					TestCase.assertEquals(elevationTileValues.tilePixels[y][x],
							pixelValue);
					TestCase.assertEquals(
							elevationTileValues.tilePixelsFlat[(y * width) + x],
							pixelValue);
					TestCase.assertEquals(
							elevationTileValues.tileUnsignedPixels[y][x],
							unsignedPixelValue);
					TestCase.assertEquals(
							elevationTileValues.tileUnsignedPixelsFlat[(y * width)
									+ x], unsignedPixelValue);
				}
				if ((griddedCoverage.getDataNull() != null && unsignedPixelValue == griddedCoverage
						.getDataNull())
						|| (griddedCoverage.getDataMissing() != null && unsignedPixelValue == griddedCoverage
								.getDataMissing())) {
					TestCase.assertNull(elevationValue);
				} else {
					TestCase.assertEquals(
							(unsignedPixelValue * griddedTile.getScale() + griddedTile
									.getOffset())
									* griddedCoverage.getScale()
									+ griddedCoverage.getOffset(),
							elevationValue);
				}
			}
		}

		// Test the individually built list of pixel values vs the full
		// returned array
		TestCase.assertEquals(pixelValuesList.size(), pixelValues.length);
		for (int i = 0; i < pixelValuesList.size(); i++) {
			TestCase.assertEquals((short) pixelValuesList.get(i),
					pixelValues[i]);
		}

		TileMatrix tileMatrix = elevationTiles.getTileDao().getTileMatrix(
				tileRow.getZoomLevel());
		BoundingBox boundingBox = TileBoundingBoxUtils.getBoundingBox(
				tileMatrixSet.getBoundingBox(), tileMatrix,
				tileRow.getTileColumn(), tileRow.getTileRow());
		ElevationTileResults elevationTileResults = elevationTiles
				.getElevations(boundingBox);
		if (elevationTileValues != null) {
			TestCase.assertEquals(elevationTileValues.tileElevations.length,
					elevationTileResults.getElevations().length);
			TestCase.assertEquals(elevationTileValues.tileElevations[0].length,
					elevationTileResults.getElevations()[0].length);
			TestCase.assertEquals(
					elevationTileValues.tileElevationsFlat.length,
					elevationTileResults.getElevations().length
							* elevationTileResults.getElevations()[0].length);
			for (int y = 0; y < elevationTileResults.getElevations().length; y++) {
				for (int x = 0; x < elevationTileResults.getElevations()[0].length; x++) {
					if (algorithm == ElevationTilesAlgorithm.BICUBIC) {
						// TODO
					//	TestCase.assertEquals(
					//			elevationTileValues.tileElevations[y][x],
					//			elevationTileResults.getElevations()[y][x]);
					} else {
						TestCase.assertEquals(
								elevationTileValues.tileElevations[y][x],
								elevationTileResults.getElevations()[y][x]);
					}
				}
			}
		}

	}

	/**
	 * Test performing elevation queries
	 * 
	 * @param geoPackage
	 * @param elevationTiles
	 * @param tileMatrixSet
	 * @param algorithm
	 * @param allowNulls
	 * @throws SQLException
	 */
	private static void testElevationQueries(GeoPackage geoPackage,
			ElevationTiles elevationTiles, TileMatrixSet tileMatrixSet,
			ElevationTilesAlgorithm algorithm, boolean allowNulls)
			throws SQLException {

		// Determine an alternate projection
		BoundingBox boundingBox = tileMatrixSet.getBoundingBox();
		SpatialReferenceSystemDao srsDao = geoPackage
				.getSpatialReferenceSystemDao();
		long srsId = tileMatrixSet.getSrsId();
		SpatialReferenceSystem srs = srsDao.getOrCreate(srsId);

		long epsg = srs.getOrganizationCoordsysId();
		Projection projection = ProjectionFactory.getProjection(srs);
		long requestEpsg = -1;
		if (epsg == ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM) {
			requestEpsg = ProjectionConstants.EPSG_WEB_MERCATOR;
		} else {
			requestEpsg = ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM;
		}
		Projection requestProjection = ProjectionFactory
				.getProjection(requestEpsg);
		ProjectionTransform elevationToRequest = projection
				.getTransformation(requestProjection);
		BoundingBox projectedBoundingBox = elevationToRequest
				.transform(boundingBox);

		// Get a random coordinate
		double latitude = (projectedBoundingBox.getMaxLatitude() - projectedBoundingBox
				.getMinLatitude())
				* Math.random()
				+ projectedBoundingBox.getMinLatitude();
		double longitude = (projectedBoundingBox.getMaxLongitude() - projectedBoundingBox
				.getMinLongitude())
				* Math.random()
				+ projectedBoundingBox.getMinLongitude();

		// Test getting the elevation of a single coordinate
		ElevationTiles elevationTiles2 = new ElevationTiles(geoPackage,
				elevationTiles.getTileDao(), requestProjection);
		elevationTiles2.setAlgorithm(algorithm);
		Double elevation = elevationTiles2.getElevation(latitude, longitude);
		if (!allowNulls) {
			TestCase.assertNotNull(elevation);
		}

		// Build a random bounding box
		double minLatitude = (projectedBoundingBox.getMaxLatitude() - projectedBoundingBox
				.getMinLatitude())
				* Math.random()
				+ projectedBoundingBox.getMinLatitude();
		double minLongitude = (projectedBoundingBox.getMaxLongitude() - projectedBoundingBox
				.getMinLongitude())
				* Math.random()
				+ projectedBoundingBox.getMinLongitude();
		double maxLatitude = (projectedBoundingBox.getMaxLatitude() - minLatitude)
				* Math.random() + minLatitude;
		double maxLongitude = (projectedBoundingBox.getMaxLongitude() - minLongitude)
				* Math.random() + minLongitude;

		BoundingBox requestBoundingBox = new BoundingBox(minLongitude,
				maxLongitude, minLatitude, maxLatitude);
		ElevationTileResults elevations = elevationTiles2
				.getElevations(requestBoundingBox);
		TestCase.assertNotNull(elevations);
		TestCase.assertNotNull(elevations.getElevations());
		TestCase.assertEquals(elevations.getElevations()[0].length,
				elevations.getWidth());
		TestCase.assertEquals(elevations.getElevations().length,
				elevations.getHeight());
		TestCase.assertNotNull(elevations.getTileMatrix());
		TestCase.assertTrue(elevations.getZoomLevel() >= 0);
		TestCase.assertTrue(elevations.getElevations().length > 0);
		TestCase.assertTrue(elevations.getElevations()[0].length > 0);
		for (int y = 0; y < elevations.getElevations().length; y++) {
			for (int x = 0; x < elevations.getElevations()[y].length; x++) {
				TestCase.assertEquals(elevations.getElevations()[y][x],
						elevations.getElevation(y, x));
			}
		}

		int specifiedWidth = 50;
		int specifiedHeight = 100;
		elevationTiles2.setWidth(specifiedWidth);
		elevationTiles2.setHeight(specifiedHeight);

		elevations = elevationTiles2.getElevations(requestBoundingBox);
		TestCase.assertNotNull(elevations);
		TestCase.assertNotNull(elevations.getElevations());
		TestCase.assertEquals(elevations.getElevations()[0].length,
				elevations.getWidth());
		TestCase.assertEquals(elevations.getElevations().length,
				elevations.getHeight());
		TestCase.assertNotNull(elevations.getTileMatrix());
		TestCase.assertTrue(elevations.getZoomLevel() >= 0);
		TestCase.assertTrue(elevations.getElevations().length > 0);
		TestCase.assertTrue(elevations.getElevations()[0].length > 0);
		TestCase.assertEquals(specifiedHeight, elevations.getHeight());
		TestCase.assertEquals(specifiedWidth, elevations.getWidth());
		for (int y = 0; y < specifiedHeight; y++) {
			for (int x = 0; x < specifiedWidth; x++) {
				TestCase.assertEquals(elevations.getElevations()[y][x],
						elevations.getElevation(y, x));
			}
		}

		elevations = elevationTiles2.getElevationsUnbounded(requestBoundingBox);
		TestCase.assertNotNull(elevations);
		TestCase.assertNotNull(elevations.getElevations());
		TestCase.assertEquals(elevations.getElevations()[0].length,
				elevations.getWidth());
		TestCase.assertEquals(elevations.getElevations().length,
				elevations.getHeight());
		TestCase.assertNotNull(elevations.getTileMatrix());
		TestCase.assertTrue(elevations.getZoomLevel() >= 0);
		TestCase.assertTrue(elevations.getElevations().length > 0);
		TestCase.assertTrue(elevations.getElevations()[0].length > 0);
		TestCase.assertEquals(
				elevations.getElevations()[0].length,
				elevations.getElevations()[elevations.getElevations().length - 1].length);
		for (int y = 0; y < elevations.getElevations().length; y++) {
			for (int x = 0; x < elevations.getElevations()[y].length; x++) {
				TestCase.assertEquals(elevations.getElevations()[y][x],
						elevations.getElevation(y, x));
			}
		}
	}

	/**
	 * Test a random bounding box query
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param elevationTileValues
	 *            elevation tile values
	 * @param algorithm
	 *            algorithm
	 * @param allowNulls
	 *            allow null elevations
	 * @throws Exception
	 */
	public static void testRandomBoundingBox(GeoPackage geoPackage,
			ElevationTileValues elevationTileValues,
			ElevationTilesAlgorithm algorithm, boolean allowNulls)
			throws Exception {

		// Verify the elevation shows up as an elevation table and not a tile
		// table
		List<String> tilesTables = geoPackage.getTileTables();
		List<String> elevationTables = ElevationTiles.getTables(geoPackage);
		TestCase.assertFalse(elevationTables.isEmpty());
		for (String tilesTable : tilesTables) {
			TestCase.assertFalse(elevationTables.contains(tilesTable));
		}

		TileMatrixSetDao dao = geoPackage.getTileMatrixSetDao();
		TestCase.assertTrue(dao.isTableExists());

		for (String elevationTable : elevationTables) {

			TileMatrixSet tileMatrixSet = dao.queryForId(elevationTable);

			TileDao tileDao = geoPackage.getTileDao(tileMatrixSet);
			ElevationTiles elevationTiles = new ElevationTiles(geoPackage,
					tileDao);
			elevationTiles.setAlgorithm(algorithm);

			int specifiedWidth = (int) (Math.random() * 100.0) + 1;
			int specifiedHeight = (int) (Math.random() * 100.0) + 1;
			elevationTiles.setWidth(specifiedWidth);
			elevationTiles.setHeight(specifiedHeight);

			BoundingBox boundingBox = tileMatrixSet.getBoundingBox();

			// Build a random bounding box
			double minLatitude = (boundingBox.getMaxLatitude() - boundingBox
					.getMinLatitude())
					* Math.random()
					+ boundingBox.getMinLatitude();
			double minLongitude = (boundingBox.getMaxLongitude() - boundingBox
					.getMinLongitude())
					* Math.random()
					+ boundingBox.getMinLongitude();
			double maxLatitude = (boundingBox.getMaxLatitude() - minLatitude)
					* Math.random() + minLatitude;
			double maxLongitude = (boundingBox.getMaxLongitude() - minLongitude)
					* Math.random() + minLongitude;

			BoundingBox requestBoundingBox = new BoundingBox(minLongitude,
					maxLongitude, minLatitude, maxLatitude);

			ElevationTileResults elevations = elevationTiles
					.getElevations(requestBoundingBox);

			TestCase.assertNotNull(elevations);
			TestCase.assertNotNull(elevations.getElevations());
			TestCase.assertEquals(elevations.getElevations()[0].length,
					elevations.getWidth());
			TestCase.assertEquals(elevations.getElevations().length,
					elevations.getHeight());
			TestCase.assertNotNull(elevations.getTileMatrix());
			TestCase.assertTrue(elevations.getZoomLevel() >= 0);
			TestCase.assertTrue(elevations.getElevations().length > 0);
			TestCase.assertTrue(elevations.getElevations()[0].length > 0);
			TestCase.assertEquals(specifiedHeight, elevations.getHeight());
			TestCase.assertEquals(specifiedWidth, elevations.getWidth());

			// TODO delete system outs
			System.out
					.println("TILE MATRIX HEIGHT: "
							+ elevations.getTileMatrix().getMatrixHeight()
							+ ", WIDTH: "
							+ elevations.getTileMatrix().getMatrixWidth());
			System.out.println("TILE HEIGHT: "
					+ elevations.getTileMatrix().getTileHeight() + ", WIDTH: "
					+ elevations.getTileMatrix().getTileWidth());
			System.out.println("Request HEIGHT: " + specifiedHeight
					+ ", WIDTH: " + specifiedWidth);

			for (int y = 0; y < specifiedHeight; y++) {
				for (int x = 0; x < specifiedWidth; x++) {
					TestCase.assertEquals(elevations.getElevations()[y][x],
							elevations.getElevation(y, x));
					if (!allowNulls) {
						if (algorithm == ElevationTilesAlgorithm.BICUBIC) {
							// TODO
							//if (elevations.getElevations()[y][x] == null) {
								//System.out.println("y = " + y + ", x = " + x);
								//TestCase.assertNotNull(elevations.getElevations()[y][x]);
							//}
						} else {
							TestCase.assertNotNull(elevations.getElevations()[y][x]);
						}
					}
				}
			}

		}

	}

	/**
	 * Get the elevation at the coordinate
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param algorithm
	 *            algorithm
	 * @param latitude
	 *            latitude
	 * @param longitude
	 *            longitude
	 * @return elevation
	 * @throws Exception
	 */
	public static Double getElevation(GeoPackage geoPackage,
			ElevationTilesAlgorithm algorithm, double latitude,
			double longitude, long epsg) throws Exception {

		Double elevation = null;

		List<String> elevationTables = ElevationTiles.getTables(geoPackage);
		TileMatrixSetDao dao = geoPackage.getTileMatrixSetDao();

		for (String elevationTable : elevationTables) {

			TileMatrixSet tileMatrixSet = dao.queryForId(elevationTable);
			TileDao tileDao = geoPackage.getTileDao(tileMatrixSet);

			Projection requestProjection = ProjectionFactory
					.getProjection(epsg);

			// Test getting the elevation of a single coordinate
			ElevationTiles elevationTiles = new ElevationTiles(geoPackage,
					tileDao, requestProjection);
			elevationTiles.setAlgorithm(algorithm);
			elevation = elevationTiles.getElevation(latitude, longitude);
		}

		return elevation;
	}

	/**
	 * Get the elevations for the bounding box
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param algorithm
	 *            algorithm
	 * @param boundingBox
	 *            bounding box
	 * @param width
	 *            results width
	 * @param width
	 *            results height
	 * @return elevation tile results
	 * @throws Exception
	 */
	public static ElevationTileResults getElevations(GeoPackage geoPackage,
			ElevationTilesAlgorithm algorithm, BoundingBox boundingBox,
			int width, int height, long epsg) throws Exception {

		ElevationTileResults elevations = null;

		List<String> elevationTables = ElevationTiles.getTables(geoPackage);
		TileMatrixSetDao dao = geoPackage.getTileMatrixSetDao();

		for (String elevationTable : elevationTables) {

			TileMatrixSet tileMatrixSet = dao.queryForId(elevationTable);
			TileDao tileDao = geoPackage.getTileDao(tileMatrixSet);

			Projection requestProjection = ProjectionFactory
					.getProjection(epsg);

			// Test getting the elevation of a single coordinate
			ElevationTiles elevationTiles = new ElevationTiles(geoPackage,
					tileDao, requestProjection);
			elevationTiles.setAlgorithm(algorithm);
			elevationTiles.setWidth(width);
			elevationTiles.setHeight(height);
			elevations = elevationTiles.getElevations(boundingBox);
		}

		return elevations;
	}

}
