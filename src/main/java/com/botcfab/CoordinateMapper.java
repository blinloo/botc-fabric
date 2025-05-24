package com.botcfab;

import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;

public class CoordinateMapper {
    public BlockPos blockUnderLever;
    public BlockPos lever;
    public BlockPos lampsVoteMarker;
    public BlockPos triggersLampPiston;
    public BlockPos chair;
    public BlockPos sign;
    public BlockPos homeOutside;
    public BlockPos homeInside;

    public CoordinateMapper(String blockUnderLever, String lever, String lampsVoteMarker,
                            String triggersLampPiston, String chair, String sign, String homeOutside, String homeInside) {

        this.blockUnderLever = convertBlockPos(blockUnderLever);
        this.lever = convertBlockPos(lever);
        this.lampsVoteMarker= convertBlockPos(lampsVoteMarker);
        this.triggersLampPiston = convertBlockPos(triggersLampPiston);
        this.chair = convertBlockPos(chair);
        this.sign = convertBlockPos(sign);
        this.homeOutside = convertBlockPos(homeOutside);
        this.homeInside = convertBlockPos(homeInside);
    }

    public BlockPos convertBlockPos(String coords) {
        List<Integer> converted = new ArrayList<>();
        try {
            for (String s : coords.split(" ")) {
                converted.add(Integer.parseInt(s));
            }
            return new BlockPos(converted.get(0), converted.get(1), converted.get(2));
        } catch (Exception e) {
            return null;
        }

    }
}
