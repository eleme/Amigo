package me.ele.app.amigo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class ParcelBean implements Parcelable {

    private String name;

    private int id;

    public ParcelBean() {
    }

    public ParcelBean(Parcel in) {
        this.name = in.readString();
        this.id = in.readInt();
    }

    public ParcelBean(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return "ParcelBean{" +
                "name='" + name + '\'' +
                ", id=" + id +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.id);
    }

    public static final Creator<ParcelBean> CREATOR = new Creator<ParcelBean>() {
        @Override
        public ParcelBean createFromParcel(Parcel source) {
            return new ParcelBean(source);
        }

        @Override
        public ParcelBean[] newArray(int size) {
            return new ParcelBean[size];
        }
    };
}