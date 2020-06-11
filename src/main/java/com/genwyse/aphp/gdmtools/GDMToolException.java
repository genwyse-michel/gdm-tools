package com.genwyse.aphp.gdmtools;

import com.genwyse.tools.ToolException;

public class GDMToolException extends ToolException {
  private static final long serialVersionUID = 1L;

  public GDMToolException () { super (); }
  public GDMToolException (String message) { super (message); }
  public GDMToolException (String message, Throwable cause) { super (message, cause); }
  public GDMToolException (Throwable cause) { super (cause); }

}
