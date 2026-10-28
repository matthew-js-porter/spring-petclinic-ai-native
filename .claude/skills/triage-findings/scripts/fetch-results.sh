#!/usr/bin/env bash
# Fetches the latest Snyk SARIF and SonarQube Cloud issues and hotspots on main into .scan-results/.
# Takes no arguments. A failure in one source is reported and never stops the other.
set -uo pipefail

cd "$(git rev-parse --show-toplevel)" || exit 1
out=.scan-results
rm -rf "$out"
mkdir -p "$out"

# In CI the job's token (with actions: read) is passed separately, so gh does not use the token
# claude-code-action provides.
if [ -n "${SNYK_ARTIFACT_TOKEN:-}" ]; then
  export GH_TOKEN="$SNYK_ARTIFACT_TOKEN"
fi

run=$(gh run list --workflow snyk.yml --branch main --status completed --limit 20 --json databaseId,event,url \
  --jq '[.[] | select(.event != "pull_request")][0] // empty | "\(.databaseId) \(.url)"')
if [ -z "$run" ]; then
  echo "Snyk: none (no completed snyk run on main found)"
elif ! gh run download "${run%% *}" -n snyk-sarif -D "$out" > /dev/null; then
  echo "Snyk: none (run ${run#* } has no snyk-sarif artifact)"
else
  echo "Snyk: ${run#* } ($(jq -s '[.[].runs[]?.results[]?] | length' "$out"/*.sarif) finding(s))"
fi

# The project is public, so no token is sent. With no branch parameter the API reads the project's main
# branch, whatever SonarQube Cloud calls it. One page of up to 500 per type; paging.total says whether
# the cap truncated it.
key=$(sed -n 's:.*<sonar.projectKey>\(.*\)</sonar.projectKey>.*:\1:p' pom.xml)
api=https://sonarcloud.io/api
if ! issues_json=$(curl -fsS "$api/issues/search?componentKeys=$key&resolved=false&ps=500") \
  || ! hotspots_json=$(curl -fsS "$api/hotspots/search?projectKey=$key&status=TO_REVIEW&ps=500"); then
  echo "Sonar: none (could not fetch results for $key)"
  exit 0
fi
jq '.issues' <<< "$issues_json" > "$out/sonar-issues.json"
jq '.hotspots' <<< "$hotspots_json" > "$out/sonar-hotspots.json"
issues=$(jq '.issues | length' <<< "$issues_json")
hotspots=$(jq '.hotspots | length' <<< "$hotspots_json")
truncated=$(jq -s --argjson i "$issues" --argjson h "$hotspots" \
  '.[0].paging.total > $i or .[1].paging.total > $h' <<< "$issues_json$hotspots_json")
dashboard="https://sonarcloud.io/project/overview?id=$key"
jq -n --arg url "$dashboard" --arg key "$key" --arg at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --argjson i "$issues" --argjson h "$hotspots" \
  '{dashboardUrl: $url, projectKey: $key, branch: "main", fetchedAt: $at, issues: $i, hotspots: $h}' \
  > "$out/sonar-meta.json"
echo "Sonar: $dashboard ($issues issue(s), $hotspots hotspot(s), truncated: $truncated)"
