package com.fringefy.urbo;

public interface DebugListener {

      void toast(final String sMsg);
      void setField(String sId, String sVal);
      void removeField(String sId);
}
