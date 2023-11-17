# tell awk to read the whole file by setting RS to something not in the file
# set FS to space to separate each word
BEGIN {
    FS=" "
}

# print the number of words in the file
{print NF}