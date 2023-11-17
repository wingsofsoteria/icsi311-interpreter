# RS is default
# FS is comma to separate each number

BEGIN {
    FS=","
}

# adding NF before the bracket will have the program skip empty lines, however NR will still increment

{
    # add each number in the line and print

    sum=0
    for (i=1;i<=NF;i++) {
        sum += $i
    }

    # NR is set to the current line

    printf "Line %s: %s\n", NR, sum

    # add the final sum to the total

    total += sum

}

END {

    # print the total after the last line

    printf "Grand Total: %s", total

}