#!/usr/bin/env bats
# -*- sh -*-

load test_helper

# rhino doesn't support :optimizations :none

@test "rhino :advanced" {
    run lein doo rhino advanced once
    assert_success
}

@test "rhino :advanced fail" {
    run lein doo rhino advanced-fail once
    assert_failure
}
