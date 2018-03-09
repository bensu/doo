#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "electron :none" {
    run lein doo electron none-test once
    assert_success
}

@test "electron :advanced" {
    run lein doo electron advanced once
    assert_success
}

@test "electron :none fail" {
    run lein doo electron none-test-fail once
    assert_failure
}

@test "electron :advanced fail" {
    run lein doo electron advanced-fail once
    assert_failure
}
