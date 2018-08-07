import sqlite3
from sys import stderr
from os import path

from sqlalchemy import create_engine, func
from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.sql import exists

from dataobjects import FacilityType, Facility, Building


class Database(object):
    def __init__(self, debug=False):
        DB_NAME = "sqlite:///locations.sqlite"
        self.engine = create_engine(DB_NAME, echo=debug)

    def start_session(self):
        '''
        ALWAYS call session.close() whenever done with a session
        '''
        engine = self.engine
        Session = sessionmaker()
        Session.configure(bind=engine)
        session = Session()
        return session

    def createTables(self):
        engine = self.engine
        tables = [FacilityType, Facility, Building]
        for table in tables:
            if not engine.dialect.has_table(engine, table.__tablename__):
                table.__table__.create(engine)

    ###########################################################################
    # ADD METHODS
    ###########################################################################

    def addBuilding(self, building):
        session = self.start_session()
        try:
            assert (isinstance(building, Building)), "Not a Building!"
            session.add(building)
            session.commit()
        finally:
            session.close()

    def addBuildings(self, buildings):
        session = self.start_session()
        try:
            for building in buildings:
                assert (isinstance(building, Building)), "Not a Building!"
                session.add(building)
            session.commit()
        finally:
            session.close()

    def addFacility(self, facility):
        session = self.start_session()
        try:
            assert (isinstance(facility, Facility)), "Not a Facility!"
            session.add(facility)
            session.commit()
        finally:
            session.close()

    def addFacilities(self, facilities):
        session = self.start_session()
        try:
            for facility in facilities:
                assert (isinstance(facility, Facility)), "Not a Facility!"
                session.add(facility)
            session.commit()
        finally:
            session.close()

    def addFacilityType(self, facility_type):
        session = self.start_session()
        try:
            assert (isinstance(facility_type, FacilityType)), "Not a FacilityType!"
            session.add(facility_type)
            session.commit()
        finally:
            session.close()

    ###########################################################################
    # GET METHODS
    ###########################################################################

    def getBuilding(self, building_name):
        '''
        Returns None if no match
        '''
        session = self.start_session()
        try:
            b = session.query(Building).filter(func.lower(Building.name) == func.lower(building_name)).first()
        finally:
            session.close()
        return b

    def getAllBuildings(self):
        session = self.start_session()
        try:
            bs = session.query(Building).all()
        finally:
            session.close()
        return bs

    def getFacility(self, facility_id):
        '''
        Returns None if no match
        '''
        session = self.start_session()
        try:
            f = session.query(Facility).get(facility_id)
        finally:
            session.close()
        return f

    def getFacilityType(self, facility_type_name):
        '''
        Returns None if no match
        '''
        session = self.start_session()
        try:
            ft = session.query(FacilityType).get(facility_type_name)
        finally:
            session.close()
        return ft

    def getAllFacilityTypes(self):
        session = self.start_session()
        try:
            fts = session.query(FacilityType).all()
        finally:
            session.close()
        return fts

    ###########################################################################
    # "SPECIAL" GET METHODS
    ###########################################################################

    def getBuildingsLike(self, building_str):
        '''
        Returns None if no match
        '''
        session = self.start_session()
        building_str = "{}%".format(building_str)
        try:
            bs = session.query(Building).filter(Building.name.ilike(building_str))
        finally:
            session.close()
        return bs


    def getFacilitiesFromBuilding(self, building_name):
        '''
        This method exists because we cannot get attributes
        (e.g. facilities) from outside of a session
        '''
        session = self.start_session()
        try:
            b = session.query(Building).filter(func.lower(Building.name) == func.lower(building_name)).first()
            fs = b.facilities
        finally:
            session.close()
        return fs

    def getFacilitiesFromFacilityType(self, facility_type_name):
        '''
        This method exists because we cannot get attributes
        (e.g. facilities) from outside of a session
        '''
        session = self.start_session()
        try:
            ft = session.query(FacilityType).get(facility_type_name)
            fs = ft.facilities
        finally:
            session.close()
        return fs

    ###########################################################################
    # SET METHODS
    ###########################################################################

    def setFacilityLatLon(self, facility):
        if facility.lat and facility.lon:
            return
        session = self.start_session()
        try:
            building_name = facility.building_name
            b = session.query(Building).get(building_name)
            # TODO: Consider adding in the new building if not found
            # (Usually, however, the building is there, it's just mispelled)
            # If the building is not present, then location might be an issue
            if b:
                facility.lat = b.lat
                facility.lon = b.lon
        finally:
            session.close()

    def setFacilitiesLatLon(self, facilities):
        # if facility.lat and facility.lon:
        #     return
        session = self.start_session()
        try:
            for facility in facilities:
                building_name = facility.building_name
                b = session.query(Building).get(building_name)
                if b:
                    facility.lat = b.lat
                    facility.lon = b.lon
        finally:
            session.close()


def main():
    db = Database()
    db.createTables()
    bs = db.getAllBuildings()
    for b in bs:
        assert (isinstance(b, Building)), "Not a Building!"
        print b.facilities


def test():
    db = Database()
    bs = db.getBuildingsLike("bu")
    for b in bs: print b.name


# print db.getFacilitiesFromBuilding("Bloomberg Hall")
# b = db.getBuilding("hi")
# print b



if __name__ == '__main__':
    # main()
    test()
