#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "phantom :none" {
    run lein doo phantom none-test once
    assert_success
}

@test "phantom :advanced" {
    run lein doo phantom advanced once
    assert_success
}

@test "phantom :none fail" {
    run lein doo phantom none-test-fail once
    assert_failure
}

@test "phantom :advanced fail" {
    run lein doo phantom advanced-fail once
    assert_failure
}
