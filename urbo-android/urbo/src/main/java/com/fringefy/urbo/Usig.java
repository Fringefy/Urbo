package com.fringefy.urbo;

import java.util.List;

/**
 * A visual urnban signature
 */
class Usig {

// Inner Types

    static class Una {
        public double azimuth;
        public String unaData;

        private Una(double camAzimuth, String sUna) {
            azimuth = camAzimuth;
            unaData = sUna;
        }
    }


// Fields

    Una[] unas;

}
