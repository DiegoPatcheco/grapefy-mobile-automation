if [ -z "$BROWSERSTACK_USERNAME" ] || [ -z "$BROWSERSTACK_ACCESS_KEY" ]; then
  echo "Error: BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY must be set in the environment." >&2
  exit 1
fi

./mvnw clean test -Dcucumber.feature="src/test/resources/features" -Dcucumber.filter.tags="@single" -DRUN_ON_BROWSERSTACK=true
