package info.ivicel.steam.slowdowng;

import java.util.List;

/**
 * Created by sedny on 28/08/2017.
 */

public class ActivateResult {
    private String key;
    private String result;
    private String details;
    private List<PackageDetail> packages;
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public List<PackageDetail> getPackages() {
        return packages;
    }
    
    public void setPackages(
            List<PackageDetail> packages) {
        this.packages = packages;
    }
    
    public class PackageDetail {
        private String subid;
        private String subName;
    
        public String getSubid() {
            return subid;
        }
    
        public void setSubid(String subid) {
            this.subid = subid;
        }
    
        public String getSubName() {
            return subName;
        }
    
        public void setSubName(String subName) {
            this.subName = subName;
        }
    }
}
