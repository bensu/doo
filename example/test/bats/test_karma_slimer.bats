#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "karma-slimer :none" {
    run lein doo karma-slimer none-test once
    assert_success
}

@test "karma-slimer :advanced" {
    run lein doo karma-slimer advanced once
    assert_success
}

@test "karma-slimer :none fail" {
    run lein doo karma-slimer none-test-fail once
    assert_failure
}

@test "karma-slimer :advanced fail" {
    run lein doo karma-slimer advanced-fail once
    assert_failure
}
