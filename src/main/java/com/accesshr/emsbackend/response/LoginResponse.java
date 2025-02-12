package com.accesshr.emsbackend.response;

import lombok.Data;

@Data
public class LoginResponse {

    private String message;
    private Boolean status;
    private String role;
    private String token;
    private String firstName;
    private String lastName;
    private String employeeId;

    // Constructor with basic fields
    public LoginResponse(String message, Boolean status, String role) {
        this.message = message;
        this.status = status;
        this.role = role;
    }

    // Constructor with token
    public LoginResponse(String message, Boolean status, String role, String token) {
        this.message = message;
        this.status = status;
        this.role = role;
        this.token = token;
    }

    // Constructor with all fields
    public LoginResponse(String message, Boolean status, String role, String token, String firstName, String lastName, String employeeId) {
        this.message = message;
        this.status = status;
        this.role = role;
        this.token = token;
        this.firstName = firstName;
        this.lastName = lastName;
        this.employeeId = employeeId;
    }

    // Default constructor
    public LoginResponse() {}

    @Override
    public String toString() {
        return "LoginResponse{" +
                "message='" + message + '\'' +
                ", status=" + status +
                ", role='" + role + '\'' +
                ", token='" + token + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", employeeId='" + employeeId + '\'' +
                '}';
    }
}