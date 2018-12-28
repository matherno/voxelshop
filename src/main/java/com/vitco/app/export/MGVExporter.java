package com.vitco.app.export;

import com.vitco.app.core.data.Data;
import com.vitco.app.core.data.container.Voxel;
import com.vitco.app.layout.content.console.ConsoleInterface;
import com.vitco.app.util.components.progressbar.ProgressDialog;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MGVExporter extends AbstractExporter {

    public MGVExporter(File exportTo, Data data, ProgressDialog dialog, ConsoleInterface console) throws IOException {
        super(exportTo, data, dialog, console);
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

        fileOut.writeUTF8String("\n");
        fileOut.writeUTF8String("voxels\n");
        Voxel[] voxels = data.getVisibleLayerVoxel();
        for (int i = 0; i < voxels.length; i++) {
            Voxel voxel = voxels[i];
            setProgress((i / (float) voxels.length) * 100);
            Color col = voxel.getColor();

            int x = voxel.x * -1;
            int y = voxel.y * -1;
            int z = voxel.z;
            int colIdx = colourRGBToIndex.get(col.getRGB());

            x--;

            fileOut.writeUTF8String(x + "," + y + "," + z + "," + colIdx + "\n");
        }
        return true;
    }
}
