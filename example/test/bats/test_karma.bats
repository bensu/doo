#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "karma :none" {
    run lein doo browsers none-test once
    assert_success
}

@test "karma :advanced" {
    run lein doo browsers advanced once
    assert_success
}

@test "karma :none fail" {
    run lein doo browsers none-test-fail once
    assert_failure
}

@test "karma :advanced fail" {
    run lein doo browsers advanced-fail once
    assert_failure
}