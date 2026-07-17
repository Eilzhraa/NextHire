package com.example.nexthire;

import java.io.Serializable;

public class Job implements Serializable {
    private String jobTitle, companyName, salary, status;
    private String description, scope, requirements;
    private String phone, email, website;
    private double latitude, longitude;

    private double distanceKm = -1;

    public Job(String jobTitle, String companyName, String salary, String status,
               String description, String scope, String requirements,
               String phone, String email, String website, double latitude, double longitude) {
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.salary = salary;
        this.status = status;
        this.description = description;
        this.scope = scope;
        this.requirements = requirements;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getJobTitle() { return jobTitle; }
    public String getCompanyName() { return companyName; }
    public String getSalary() { return salary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public String getScope() { return scope; }
    public String getRequirements() { return requirements; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }


}