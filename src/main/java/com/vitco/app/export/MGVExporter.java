package com.vitco.app.export;

import com.vitco.app.core.data.Data;
import com.vitco.app.core.data.container.Voxel;
import com.vitco.app.layout.content.console.ConsoleInterface;
import com.vitco.app.util.components.progressbar.ProgressDialog;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Math.abs;

public class MGVExporter extends AbstractExporter {
    public boolean includeInteriorVoxels = false;

    public MGVExporter(File exportTo, Data data, ProgressDialog dialog, ConsoleInterface console) throws IOException {
        super(exportTo, data, dialog, console);
    }

    public void includeInteriorVoxels(boolean include) {
        includeInteriorVoxels = include;
    }

    private class MGVVoxel {
        public int x = 0;
        public int y = 0;
        public int z = 0;
        public int colIdx = 0;

        MGVVoxel() {}
        MGVVoxel(int x, int y, int z, int colIdx) { this.x = x; this.y = y; this.z = z; this.colIdx = colIdx; }
    }

    @Override
    protected boolean writeFile() throws IOException {

        fileOut.writeUTF8String("mgv\n");
        fileOut.writeUTF8String("version 1.0\n");
        fileOut.writeUTF8String("\n");
        fileOut.writeUTF8String("colours\n");

        HashMap<Integer, Integer> colourRGBToIndex = new HashMap<>();
        int idx = 0;
        for (int col : data.getVoxelColorList().toArray()) {
            colourRGBToIndex.put(col, idx);
            idx++;
            Color colour = new Color(col);
            int r = colour.getRed();
            int g = colour.getGreen();
            int b = colour.getBlue();
            fileOut.writeUTF8String(r + "," + g + "," + b + "\n");
        }

        Voxel[] voxels = data.getVisibleLayerVoxel();
        ArrayList<MGVVoxel> exportVoxels = new ArrayList<>();
        for (int i = 0; i < voxels.length; i++) {
            Voxel voxel = voxels[i];
            setProgress((i / (float) voxels.length) * 100);
            Color col = voxel.getColor();

            int x = voxel.x * -1;
            int y = voxel.y * -1;
            int z = voxel.z;
            int colIdx = colourRGBToIndex.get(col.getRGB());

            x--;

            exportVoxels.add(new MGVVoxel(x, y, z, colIdx));
        }


        fileOut.writeUTF8String("\n");
        fileOut.writeUTF8String("voxels\n");

        idx = 0;
        for (MGVVoxel voxel : exportVoxels) {
            setProgress((idx++ / (float) exportVoxels.size()) * 100);
            boolean exportVoxel = true;

            if (!includeInteriorVoxels) {
                //  brute force method of filtering out voxels that are fully interior
                int adjacentVoxels = 0;
                for (MGVVoxel voxel2 : exportVoxels) {
                    int distanceAway = abs(voxel.x - voxel2.x) + abs(voxel.y - voxel2.y) + abs(voxel.z - voxel2.z);
                    if (distanceAway == 1)
                        ++adjacentVoxels;
                    if (adjacentVoxels >= 6) {
                        exportVoxel = false;
                        break;
                    }
                }
            }

            if (exportVoxel)
                fileOut.writeUTF8String(voxel.x + "," + voxel.y + "," + voxel.z + "," + voxel.colIdx + "\n");
        }

        return true;
    }
}

