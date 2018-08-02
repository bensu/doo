#!/usr/bin/env bats
# -*- sh -*-

load test_helper

# optimization levels do not matter for planck

@test "planck" {
    skip "planck is not installed on CircleCI"
    run lein doo planck none-test once
    assert_success
}

@test "planck fail" {
    skip "planck is not installed on CircleCI"
    run lein doo planck none-test-fail once
    assert_failure
}
