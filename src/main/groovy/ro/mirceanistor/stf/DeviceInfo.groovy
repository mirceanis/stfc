package ro.mirceanistor.stf

class DeviceInfo {
    DeviceInfo(def serial, def width, def height, def sdk, def name, def model, def connectionString, def notes, def using, def ownerEmail) {
        this.serial = serial
        this.width = width
        this.height = height
        this.sdk = sdk
        this.name = name
        this.model = model
        this.remoteConnectUrl = connectionString
        this.notes = notes
        this.using = using
        this.ownerEmail = ownerEmail
    }

    String serial
    int width
    int height
    int sdk
    String name
    String model
    String remoteConnectUrl
    String notes
    boolean using
    String ownerEmail

    @Override
    public String toString() {
        return "DeviceInfo{" +
                " sdk=" + sdk +
                ", serial='" + serial + '\'' +
                ", model='" + model + '\'' +
                ", name='" + name + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", remoteConnectUrl='" + remoteConnectUrl + '\'' +
                ", notes='" + notes + '\'' +
                ", using=" + using +
                ", ownerEmail='" + ownerEmail + '\'' +
                '}';
    }
}