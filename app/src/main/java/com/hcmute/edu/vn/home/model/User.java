package com.hcmute.edu.vn.home.model;

public class User {
    private String username;
    private String password;
    private String fullName;
    private String dob;
    private String gender;
    private String address;
    private double height;
    private double weight;

    public User(String username, String password, String fullName, String dob, String gender, String address, double height, double weight) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.dob = dob;
        this.gender = gender;
        this.address = address;
        this.height = height;
        this.weight = weight;
    }

    // Getter methods
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getDob() { return dob; }
    public String getGender() { return gender; }
    public String getAddress() { return address; }
    public double getHeight() { return height; }
    public double getWeight() { return weight; }
}