import pandas as pd

# Load the CSV file
df = pd.read_csv('random_users.csv', on_bad_lines='skip')

# Drop duplicate userId rows, keeping the first occurrence
df_unique = df.drop_duplicates(subset='userId', keep='first')

# Save the cleaned data back to a CSV file
df_unique.to_csv('cleaned_file.csv', index=False)
