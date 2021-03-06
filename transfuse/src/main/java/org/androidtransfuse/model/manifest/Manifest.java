/**
 * Copyright 2013 John Ericksen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.androidtransfuse.model.manifest;

import java.util.ArrayList;
import java.util.List;

import org.androidtransfuse.model.Mergeable;
import org.androidtransfuse.processor.MergeCollection;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * attributes:
 * xmlns:android="http://schemas.android.com/apk/res/android"
 * package="string"
 * android:sharedUserId="string"
 * android:sharedUserLabel="string resource"
 * android:versionCode="integer"
 * android:versionName="string"
 * android:installLocation=["auto" | "internalOnly" | "preferExternal"]
 * <p/>
 * must contain:
 * <application>
 * <p/>
 * can contain:
 * <instrumentation>
 * <permission>
 * <permission-group>
 * <permission-tree>
 * <uses-configuration>
 * <uses-permission>
 * <uses-sdk>
 * <compatible-screens>
 *
 * @author John Ericksen
 */
@XStreamAlias("manifest")
public class Manifest extends Mergeable {

    public static final String NAMESPACE = "http://androidtransfuse.org";

    @XStreamAlias("xmlns:t")
    @XStreamAsAttribute
    private String transfuseNamespace;
    @XStreamAlias("xmlns:android")
    @XStreamAsAttribute
    private final String namespace = "http://schemas.android.com/apk/res/android";
    @XStreamAlias("package")
    @XStreamAsAttribute
    private String applicationPackage;
    @XStreamAlias("android:sharedUserId")
    @XStreamAsAttribute
    private String sharedUserId;
    @XStreamAlias("android:sharedUserLabel")
    @XStreamAsAttribute
    private String sharedUserLabel;
    @XStreamAlias("android:versionCode")
    @XStreamAsAttribute
    private Integer versionCode;
    @XStreamAlias("android:versionName")
    @XStreamAsAttribute
    private String versionName;
    @XStreamAlias("android:installLocation")
    @XStreamAsAttribute
    private InstallLocation installLocation;

    @XStreamImplicit(itemFieldName = "uses-permission")
    private List<UsesPermission> usesPermissions = new ArrayList<UsesPermission>();
    @XStreamImplicit(itemFieldName = "uses-sdk")
    private List<UsesSDK> usesSDKs = new ArrayList<UsesSDK>();
    @XStreamImplicit(itemFieldName = "application")
    private List<Application> applications = new ArrayList<Application>();
    @XStreamImplicit(itemFieldName = "instrumentation")
    private List<Instrumentation> instrumentations = new ArrayList<Instrumentation>();
    @XStreamImplicit(itemFieldName = "permission")
    private List<Permission> permissions = new ArrayList<Permission>();
    @XStreamImplicit(itemFieldName = "permission-group")
    private List<PermissionGroup> permissionGroups = new ArrayList<PermissionGroup>();
    @XStreamImplicit(itemFieldName = "permission-tree")
    private List<PermissionTree> permissionTrees = new ArrayList<PermissionTree>();
    @XStreamImplicit(itemFieldName = "supports-screens")
    private List<SupportsScreens> supportsScreens = new ArrayList<SupportsScreens>();
    @XStreamImplicit(itemFieldName = "uses-feature")
    private List<UsesFeature> usesFeatures = new ArrayList<UsesFeature>();
    @XStreamImplicit(itemFieldName = "uses-configuration")
    private List<UsesConfiguration> usesConfigurations = new ArrayList<UsesConfiguration>();
    @XStreamImplicit(itemFieldName = "compatible-screens")
    private List<CompatibleScreens> compatibleScreens = new ArrayList<CompatibleScreens>();

    public String getApplicationPackage() {
        return applicationPackage;
    }

    public void setApplicationPackage(String applicationPackage) {
        this.applicationPackage = applicationPackage;
    }

    public void updateNamespace(){
        transfuseNamespace = NAMESPACE;
    }

    public String getSharedUserId() {
        return sharedUserId;
    }

    public void setSharedUserId(String sharedUserId) {
        this.sharedUserId = sharedUserId;
    }

    public String getSharedUserLabel() {
        return sharedUserLabel;
    }

    public void setSharedUserLabel(String sharedUserLabel) {
        this.sharedUserLabel = sharedUserLabel;
    }

    public Integer getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public InstallLocation getInstallLocation() {
        return installLocation;
    }

    public void setInstallLocation(InstallLocation installLocation) {
        this.installLocation = installLocation;
    }

    @MergeCollection(collectionType = ArrayList.class, type = Application.class)
    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public List<Instrumentation> getInstrumentations() {
        return instrumentations;
    }

    public void setInstrumentations(List<Instrumentation> instrumentations) {
        this.instrumentations = instrumentations;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    @MergeCollection(collectionType = ArrayList.class, type = Permission.class)
    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public List<PermissionGroup> getPermissionGroups() {
        return permissionGroups;
    }

    public void setPermissionGroups(List<PermissionGroup> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    public List<PermissionTree> getPermissionTrees() {
        return permissionTrees;
    }

    public void setPermissionTrees(List<PermissionTree> permissionTrees) {
        this.permissionTrees = permissionTrees;
    }

    public List<UsesConfiguration> getUsesConfigurations() {
        return usesConfigurations;
    }

    public void setUsesConfigurations(List<UsesConfiguration> usesConfigurations) {
        this.usesConfigurations = usesConfigurations;
    }

    @MergeCollection(collectionType = ArrayList.class, type = UsesPermission.class)
    public List<UsesPermission> getUsesPermissions() {
        return usesPermissions;
    }

    public void setUsesPermissions(List<UsesPermission> usesPermissions) {
        this.usesPermissions = usesPermissions;
    }

    @MergeCollection(collectionType = ArrayList.class, type = UsesSDK.class)
    public List<UsesSDK> getUsesSDKs() {
        return usesSDKs;
    }

    public void setUsesSDKs(List<UsesSDK> usesSDKs) {
        this.usesSDKs = usesSDKs;
    }

    public List<SupportsScreens> getSupportsScreens() {
        return supportsScreens;
    }

    public void setSupportsScreens(List<SupportsScreens> supportsScreens) {
        this.supportsScreens = supportsScreens;
    }

    @MergeCollection(collectionType = ArrayList.class, type = UsesFeature.class)
    public List<UsesFeature> getUsesFeatures() {
        return usesFeatures;
    }

    public void setUsesFeatures(List<UsesFeature> usesFeatures) {
        this.usesFeatures = usesFeatures;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTransfuseNamespace() {
        return transfuseNamespace;
    }

    public List<CompatibleScreens> getCompatibleScreens() {
        return compatibleScreens;
    }

    public void setCompatibleScreens(List<CompatibleScreens> compatibleScreens) {
        this.compatibleScreens = compatibleScreens;
    }

    public void updatePackages() {
        if(applicationPackage != null){
            for (Application application : applications) {
                application.updatePackage(applicationPackage);
            }
        }
    }
}
