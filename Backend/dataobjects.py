from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import create_engine
from sqlalchemy import inspect

from sqlalchemy.schema import PrimaryKeyConstraint, ForeignKeyConstraint
from sqlalchemy.orm import relationship
from sqlalchemy import (
    Column,
    Integer,
    String,
    Text,
    Boolean,
    ForeignKey,
    Date,
    DateTime,
    Sequence,
    Float
)
import datetime

import requests

Base = declarative_base()


class FacilityType(Base):
    '''
    E.g. name="laundry", "kitchen", "printer"
    '''
    __tablename__ = 'facility_types'
    name = Column(String, primary_key=True)
    facilities = relationship("Facility", backref="facility_types")

    def __init__(self, name):
        self.name = name

    def toString(self):
        return "FacilityType: {}".format(self.name)

    def toDict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns }


class Facility(Base):
    __tablename__ = 'facilities'
    id = Column(Integer, primary_key=True)
    facility_type_name = Column(String, ForeignKey('facility_types.name'), nullable=False)
    building_name = Column(String, ForeignKey('buildings.name'), nullable=False)
    description = Column(Text)
    lat = Column(Float)
    lon = Column(Float)

    def __init__(self, facility_type_name, building_name, description, lat=None, lon=None):
        self.facility_type_name = facility_type_name
        self.building_name = building_name
        self.description = description
        self.lat = lat
        self.lon = lon

    def toString(self):
        return "FacilityType: {}, Building: {}, Description: {}, Lat: {}, Lon: {}".format(
            self.facility_type_name, self.building_name, self.description, self.lat, self.lon)

    def toDictNoType(self):
        dict = {}
        dict["1"] = "Deprecated"
        order = {"id":"0", "building_name":"2", "description":"3","lat":"4", "lon":"5"}
        for c in self.__table__.columns:
            if c.name != "facility_type_name":
                dict[order[c.name]] = getattr(self, c.name)

        return dict


    def toDict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}


class Building(Base):
    __tablename__ = 'buildings'
    name = Column(String, primary_key=True)
    lat = Column(Float)
    lon = Column(Float)
    facilities = relationship("Facility", backref="buildings")

    def __init__(self, name, lat=None, lon=None):
        self.name = name
        self.lat = lat
        self.lon = lon

    def setLatLon(self):
        api = "https://maps.googleapis.com/maps/api/geocode/json?address={0},+Princeton,+NJ,+USA&key=%20AIzaSyCy1_AXCEDXUl3GVBlH6-4iWtJV0VBWaW0".format(
            self.name)
        req = requests.get(api)
        res = req.json()
        geodata = dict()
        if len(res['results']) == 0:
            return
        result = res['results'][0]
        self.lat = result['geometry']['location']['lat']
        self.lon = result['geometry']['location']['lng']

    def toString(self):
        return "Building: {}, Lat: {}, Lon: {}".format(
            self.name, self.lat, self.lon)

    def toDictNoLatLon(self):
        dict = {}
        for c in self.__table__.columns:
            if c.name == "name":
                dict['0'] = getattr(self, c.name)

        return dict

    def toDict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}


def main():
    pass


if __name__ == '__main__':
    main()

