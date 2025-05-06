package com.botcfab;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ImportExcelCoordinates {
    //Takes in csv file of coords and creates hash key of it
    public static List<Map<String, String>> read(String filePath) {
        File file = new File(filePath);
        List<Map<String, String>> response = new LinkedList<Map<String, String>>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        Map <String, CoordinateMapper> mc = new HashMap<>();

        try {
            MappingIterator<Map<String, String>> iterator = mapper.reader(Map.class)
                    .with(schema)
                    .readValues(file);
            while (iterator.hasNext()) {
                //response.add(iterator.next());
                Map <String, String> mappies = iterator.next();
                CoordinateMapper m1 mappies[0]
                mappies.forEach((k,v) -> {

                });
            }
            return response;
        }
        catch (IOException e) {
            return null;
        }
    }
}