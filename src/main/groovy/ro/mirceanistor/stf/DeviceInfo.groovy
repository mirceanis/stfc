/*******************************************************************************
 * Copyright 2017 Mircea Nistor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package ro.mirceanistor.stf

/**
 * A class that represents the device properties we care about
 */
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
                '}'
    }
}
