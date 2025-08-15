package com.acm.clip;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.Serializable;
import java.util.List;

public class Clip implements Serializable {

	private static final long serialVersionUID = 1L;

	public int id;
	public long crcValue;
	public boolean isEmpty;
	public boolean isString;
	public boolean isFile;
	public boolean isImage;
	public boolean isLocked;
	public String stringObject;
	public List<File> fileObject;
	transient public Image imageObject;
	transient public Transferable clip;

	public Clip(int id, long crcValue, boolean isEmpty, boolean isString, boolean isFile, boolean isImage, boolean isLocked) {
		this.id = id;
		this.crcValue = crcValue;
		this.isEmpty = isEmpty;
		this.isString = isString;
		this.isFile = isFile;
		this.isImage = isImage;
		this.isLocked = isLocked;
		stringObject = null;
		fileObject = null;
		clip = null;
	}

	public void deleteClip() {
		isEmpty = true;
		isString = false;
		isImage = false;
		isFile = false;
		isLocked = false;
		imageObject = null;
		stringObject = null;
		fileObject = null;
		clip = null;
	}

	public void setNewString(String stringObject, Transferable clip) {
		isEmpty = false;
		isString = true;
		isImage = false;
		isFile = false;
		this.stringObject = stringObject;
		this.clip = clip;
		imageObject = null;
		fileObject = null;
	}

	public void setNewImage(Image imageObject, Transferable clip) {
		isEmpty = false;
		isString = false;
		isImage = true;
		isFile = false;
		this.imageObject = imageObject;
		this.clip = clip;
		stringObject = null;
		fileObject = null;
	}

	public void setNewFile(List<File> fileObject, Transferable clip) {
		isEmpty = false;
		isString = false;
		isImage = false;
		isFile = true;
		this.fileObject = fileObject;
		this.clip = clip;
		stringObject = null;
		imageObject = null;
	}
}