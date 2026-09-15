# python3 -m venv my_env
# source my_env/bin/activate
# python3 -m pip install pandas
# python3 -m pip install sqlalchemy
# python3 -m pip install psycopg2-binary
import glob
import pandas as pd
from sqlalchemy import create_engine

engine = create_engine('postgresql://postgres:postgres@localhost:5252/postgres')

first_file = True
for file in glob.glob("/Users/ben/Downloads/snyk/*.csv"):
    df = pd.read_csv(file)
    # convert all column names to lowercase
    df.columns = df.columns.str.lower()

    if first_file == True:
        df.to_sql('snyk_issue', engine, if_exists='replace', index=False)
        first_file = False
    else:
        df.to_sql('snyk_issue', engine, if_exists='append', index=False)
