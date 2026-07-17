package com.example.nexthire;

public class Application {
    private String applicantName;
    private String jobTitle;
    private String companyName;
    private String status;

    public Application() {
    }

    public Application(String applicantName, String jobTitle, String companyName, String status) {
        this.applicantName = applicantName;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.status = status;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}