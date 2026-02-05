file1 = "output.txt"
file2 = "expected_output.txt"

with open(file1) as f1, open(file2) as f2:
    lines1 = f1.readlines()
    lines2 = f2.readlines()

total_lines = max(len(lines1), len(lines2))
matching = 0
different = 0

for l1, l2 in zip(lines1, lines2):
    if l1.strip() == l2.strip():
        matching += 1
    else:
        different += 1


different += abs(len(lines1) - len(lines2))

accuracy = (matching / total_lines) * 100 if total_lines > 0 else 0

print("Total lines:", total_lines)
print("Different lines:", different)
print("Accuracy:", accuracy, "%")