package com.botcfab;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ImportExcelCoordinates {
    public static final String MOD_ID = "botc-fab";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    //Takes in csv file of coords and creates hash key of it
    public static Map<String, CoordinateMapper> read(File file) {
        CsvMapper mapper = new CsvMapper().enable(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE);
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        Map <String, CoordinateMapper> mc = new HashMap<>();

        try {
            MappingIterator<Map<String,String>> iterator = mapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(file);


            while (iterator.hasNext()) {
                Map<String,String> row = iterator.next();
                LOGGER.info(row.toString());
                String colour = row.get("Colours"); //This is the key?
                List<String> rowValues = new ArrayList<>(row.values());
                if (colour != null && !colour.isBlank()) {
                    mc.put(colour, new CoordinateMapper(
                            rowValues.get(1), rowValues.get(2), rowValues.get(3), rowValues.get(4), rowValues.get(5),
                            rowValues.get(6), rowValues.get(7), rowValues.get(8)
                    ));
                }
            }
            return mc;
        }
        catch (IOException e) {
            LOGGER.info(e.toString());
            return mc;
        }
    }
}