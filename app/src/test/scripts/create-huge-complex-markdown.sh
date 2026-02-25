#!/usr/bin/env bash
set -euo pipefail

output="realistic.md"
> "$output"

for i in {1..20}; do
  cat <<PART >> "$output"

# Document #$i – $(date --utc +%Y-%m-%dT%H:%M:%SZ)

This is sample paragraph content repeated for realistic document size testing.
Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.

## Recent log excerpt (simulated)

$(for ((j=1; j<=300; j++)); do
    seconds_ago=$(( (i*17 + j*3) % 86400 ))
    ts=$(date --utc --date "now - ${seconds_ago} seconds" +%Y-%m-%dT%H:%M:%S.%3NZ 2>/dev/null || date --utc +%Y-%m-%dT%H:%M:%S.%3NZ)

    # Rotate log level without array (POSIX-friendly)
    case $((j % 4)) in
      0) level="INFO"  ;;
      1) level="WARN"  ;;
      2) level="ERROR" ;;
      *) level="DEBUG" ;;
    esac

    user="user$((1000 + (i+j) % 9000))@example.com"
    echo "$ts [$level] Processing request id=${i}j${j} from $user – status=success"
  done)

## JSON payload sample

[
$(for ((k=1; k<=80; k++)); do
    # Smaller random payload to keep generation speed reasonable
    payload=$(head -c $((60 + (k % 140))) /dev/urandom 2>/dev/null | base64 -w 0 | head -c 120)
    cat <<JSON
  {
    "id": $((i*1000 + k)),
    "timestamp": "$(date --utc +%Y-%m-%dT%H:%M:%SZ)",
    "event": "click",
    "payload": "$payload",
    "ip": "192.168.$((i % 255)).$((k % 255))"
  }$( [[ $k -lt 80 ]] && echo "," || echo "" )
JSON
  done
)

## Recent activity log (table)

| Time                  | User                        | Action             | Amount    | Status     | Location          |
|-----------------------|-----------------------------|--------------------|-----------|------------|-------------------|
$(for ((r=1; r<=40; r++)); do
  mins_ago=$(( (i*11 + r*7) % 1440 ))
  ts=$(date --utc --date "now - ${mins_ago} minutes" +%H:%M:%S)
  user="u$(printf "%04d" $((8000 + (i+r) % 1999)))"
  action=$(printf "%s" "view" "purchase" "update" "login" "search" "cart" | shuf -n1)
  amount=$(printf "%.2f" "$(echo "scale=2; 5 + $RANDOM % 380" | bc)")
  status=$(printf "%s" "success" "success" "success" "failed" "pending" | shuf -n1)
  loc=$(printf "%s" "EU-West" "US-East" "AP-Southeast" "US-West" "EU-Central" | shuf -n1)
  printf "| %s | %s@example.com | %s | \$%s | %s | %s |\n" "$ts" "$user" "$action" "$amount" "$status" "$loc"
done)

## Image & link references

![Widget $(printf %04d $i)](https://picsum.photos/seed/doc$i/1200/800?grayscale)
→ [Open report #$i](https://demo.app/reports/$i?token=$(head -c 8 /dev/urandom | xxd -p -c 16))

PART

  # Show progress
  (( i % 200 == 0 )) && du -h "$output" | awk '{print "  → " $1 " so far (document " "'$i'")}'

done

# Big final block (~250–300 MB)
echo -e "\n\n# Large base64 attachment (simulated binary)\n\`\`\`text" >> "$output"
head -c $((280 * 1024 * 1024)) /dev/urandom | base64 -w 76 >> "$output" 2>/dev/null
echo '```' >> "$output"

echo "Done. Final size:"
du -h "$output"