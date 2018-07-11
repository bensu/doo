#!/usr/bin/env bats
# -*- sh -*-

load test_helper

# optimization levels do not matter for lumo

@test "lumo" {
    run lein doo lumo none-test once
    assert_success
}

@test "lumo fail" {
    run lein doo lumo none-test-fail once
    assert_failure
}
