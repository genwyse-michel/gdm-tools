package com.genwyse.aphp.gdmtools;

public enum GDMToolStatus {
  OK(0),
  HELP(1),  // Rendu par Tool
  WARN(2),
  ERR_BAD_CONFIG(-1),
  ERR_PROCESSING_ERROR(-2),
  ;
  
  private int value;
  private GDMToolStatus(int value) {
    this.value = value;
  }
  public int getValue() {
    return value;
  }
}