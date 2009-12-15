package ij.plugin;
import ijx.IjxImagePlus;
import ij.*;
import ij.io.*;
import ij.process.*;
import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;


/** Saves in PNG format using the ImageIO classes.  RGB images are saved
	as RGB PNGs. All other image types are saved as 8-bit PNGs. With 8-bit images,
	the value of the transparent index can be set in Edit/Options/Input-Output. */
public class PNG_Writer implements PlugIn {
    IjxImagePlus imp;

    public void run(String path) {
        imp = WindowManager.getCurrentImage();
        if (imp==null)
        	{IJ.noImage(); return;}

        if (path.equals("")) {
            SaveDialog sd = new SaveDialog("Save as PNG...", imp.getTitle(), ".png");
            String name = sd.getFileName();
            if (name==null)
                return;
            String dir = sd.getDirectory();
            path = dir + name;
        }

        try {
            writeImage(imp, path, Prefs.getTransparentIndex());
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg==null || msg.equals(""))
                msg = ""+e;
            IJ.showMessage("PNG Writer", "An error occured writing the file.\n \n" + msg);
        }
        IJ.showStatus("");
    }

	public void writeImage(IjxImagePlus imp, String path, int transparentIndex) throws Exception {
		if (transparentIndex>=0 && transparentIndex<=255 && imp.getBitDepth()==8)
			writeImageWithTransparency(imp, path, transparentIndex);
		else
			ImageIO.write(imp.getBufferedImage(), "png", new File(path));
	}
    
	void writeImageWithTransparency(IjxImagePlus imp, String path, int transparentIndex) throws Exception {
		int width = imp.getWidth();
		int  height = imp.getHeight();
		ImageProcessor ip = imp.getProcessor();
		IndexColorModel cm = (IndexColorModel)ip.getColorModel();
		int size = cm.getMapSize();
		byte[] reds = new byte[256];
		byte[] greens = new byte[256];
		byte[] blues = new byte[256];	
		cm.getReds(reds); 
		cm.getGreens(greens); 
		cm.getBlues(blues);
		cm = new IndexColorModel(8, 256, reds, greens, blues, transparentIndex);
		WritableRaster wr = cm.createCompatibleWritableRaster(width, height);
		DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
		byte[] biPixels = db.getData();
		System.arraycopy(ip.getPixels(), 0, biPixels, 0, biPixels.length);
		BufferedImage bi = new BufferedImage(cm, wr, false, null);
		ImageIO.write(bi, "png", new File(path));
	}

	/*
	void writeImageWithTransparency(IjxImagePlus imp, String path, int transparentIndex) throws Exception {
		int width = imp.getWidth();
		int  height = imp.getHeight();
		ImageProcessor ip = imp.getProcessor();
		IndexColorModel cm = (IndexColorModel)ip.getColorModel();
		int size = cm.getMapSize();
		byte[] reds = new byte[256];
		byte[] greens = new byte[256];
		byte[] blues = new byte[256];	
		cm.getReds(reds); 
		cm.getGreens(greens); 
		cm.getBlues(blues);
		cm = new IndexColorModel(8, 256, reds, greens, blues, transparentIndex);
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
		DataBuffer db = new DataBufferByte((byte[])ip.getPixels(), width*height, 0);
		WritableRaster wr = cm.createCompatibleWritableRaster(1, 1);
		SampleModel sm = wr.getSampleModel();
		sm = sm.createCompatibleSampleModel(width, height);
		Raster raster = Raster.createRaster(sm, db, null);
		bi.setData(raster);
		ImageIO.write(bi, "png", new File(path));
	}
	*/
	
	/*
	void writeImageWithTransparency(IjxImagePlus imp, String path, int transparentIndex) throws Exception {
		int width = imp.getWidth();
		int  height = imp.getHeight();
		LookUpTable lut = imp.createLut();
		int size = lut.getMapSize();
		byte [] reds = lut.getReds();
		byte [] greens = lut.getGreens();
		byte [] blues = lut.getBlues();
		IndexColorModel cm = new IndexColorModel(8, size, reds, greens, blues, transparentIndex);
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
		Graphics2D g = (Graphics2D)bi.getGraphics();
		g.drawImage(imp.getImage(), 0, 0, null);
		File f = new File(path);
		ImageIO.write(bi, "png", f);
	}
	*/

}

