package pl.karol202.paintplus.color.picker.panel;

import android.graphics.Color;
import pl.karol202.paintplus.color.HSVToRGB;
import pl.karol202.paintplus.color.RGBToHSV;

class ColorModeHSV extends ColorMode
{
	private ColorChannel channelHue;
	private ColorChannel channelSaturation;
	private ColorChannel channelValue;
	
	private HSVToRGB hsvToRGB;
	private RGBToHSV rgbToHSV;
	
	ColorModeHSV()
	{
		channelHue = new ColorChannel(this, ColorChannel.ColorChannelType.HUE);
		channelSaturation = new ColorChannel(this, ColorChannel.ColorChannelType.SATURATION);
		channelValue = new ColorChannel(this, ColorChannel.ColorChannelType.VALUE);
		
		hsvToRGB = new HSVToRGB();
		rgbToHSV = new RGBToHSV();
	}
	
	@Override
	ChannelXYSet getChannelXYSetForMainChannel(ColorChannel mainChannel)
	{
		if(mainChannel == channelHue) return new ChannelXYSet(channelValue, channelSaturation);
		else if(mainChannel == channelSaturation) return new ChannelXYSet(channelValue, channelHue);
		else if(mainChannel == channelValue) return new ChannelXYSet(channelSaturation, channelHue);
		else return null;
	}
	
	@Override
	ColorChannel[] getChannels()
	{
		return new ColorChannel[] { channelHue, channelSaturation, channelValue };
	}
	
	@Override
	int getColor()
	{
		hsvToRGB.setColor(channelHue.getValue(), channelSaturation.getValue(), channelValue.getValue());
		return Color.rgb(hsvToRGB.getR(), hsvToRGB.getG(), hsvToRGB.getB());
	}
	
	@Override
	void setColor(int color)
	{
		rgbToHSV.setColor(Color.red(color), Color.green(color), Color.blue(color));
		channelHue.setValue(rgbToHSV.getH());
		channelSaturation.setValue(rgbToHSV.getS());
		channelValue.setValue(rgbToHSV.getV());
	}
}