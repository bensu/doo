#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "firefox-headless :none" {
    run lein doo firefox-headless none-test once
    assert_success
}

@test "firefox-headless :advanced" {
    run lein doo firefox-headless advanced once
    assert_success
}

@test "firefox-headless :none fail" {
    run lein doo firefox-headless none-test-fail once
    assert_failure
}

@test "firefox-headless :advanced fail" {
    run lein doo firefox-headless advanced-fail once
    assert_failure
}
