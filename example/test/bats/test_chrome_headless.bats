#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "chrome-headless :none" {
    run lein doo chrome-headless none-test once
    assert_success
}

@test "chrome-headless :advanced" {
    run lein doo chrome-headless advanced once
    assert_success
}

@test "chrome-headless :none fail" {
    run lein doo chrome-headless none-test-fail once
    assert_failure
}

@test "chrome-headless :advanced fail" {
    run lein doo chrome-headless advanced-fail once
    assert_failure
}
