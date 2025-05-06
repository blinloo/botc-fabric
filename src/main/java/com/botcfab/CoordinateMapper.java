package com.botcfab;

import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;

public class CoordinateMapper {
    public BlockPos VOTELAMP; //Coordinates of vote lamp
    public BlockPos REDSTONEBLOCK; //Vote lamp piston activation

    public CoordinateMapper(String c) {
        VOTELAMP = convertBlockPos(c);
    }

    private BlockPos convertBlockPos(String coords) {
        List<Integer> converted = new ArrayList<>();
        for (String s : coords.split(",")) {
            converted.add(Integer.parseInt(s));
        }
        return new BlockPos(converted.get(0), converted.get(1), converted.get(2));
    }
}
