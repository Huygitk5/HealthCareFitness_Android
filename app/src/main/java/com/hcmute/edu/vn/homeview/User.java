package com.hcmute.edu.vn.homeview;

public class User {
    private String username;
    private String password;
    private String fullName;
    private String dob;
    private String gender;
    private String address;

    public User(String username, String password, String fullName, String dob, String gender, String address) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.dob = dob;
        this.gender = gender;
        this.address = address;
    }

    // Getter methods
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getDob() { return dob; }
    public String getGender() { return gender; }
    public String getAddress() { return address; }
}