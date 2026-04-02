package uk.ac.westminster.smartcampus;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api/v1")
public class ApplicationConfig extends Application {
    //auto discovers by jakarta
}