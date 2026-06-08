package com.group2.basis.se2034swp391g2.vn.edu.fpt.exception;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProfileValidationException extends RuntimeException {
    private final Map<String,String> fieldErrors = new LinkedHashMap<>();

    public ProfileValidationException addFieldError(String fieldName,String message){
        fieldErrors.put(fieldName,message);
        return this;
    }

    public boolean hasErrors(){
        return !fieldErrors.isEmpty();
    }

    public Map<String,String> getFieldErrors(){
        return fieldErrors;
    }
}
