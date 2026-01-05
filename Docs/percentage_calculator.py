from decimal import Decimal, ROUND_HALF_UP

def apply_percentage(value: Decimal, percentage: Decimal) -> Decimal:
    """
    Applies a percentage to a value.
    Positive percentage = increase
    Negative percentage = decrease
    """
    multiplier = Decimal("1") + (percentage / Decimal("100"))
    result = value * multiplier

    # Round to 2 decimal places (financial standard)
    return result.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def main():
    try:
        value_input = input("Enter base value: ")
        percentage_input = input("Enter percentage (e.g. 10 or -10): ")

        value = Decimal(value_input)
        percentage = Decimal(percentage_input)

        new_value = apply_percentage(value, percentage)

        print(f"\nBase value: {value}")
        print(f"Percentage: {percentage}%")
        print(f"New value: {new_value}")

    except Exception as e:
        print("Invalid input. Please enter numeric values only.")
        print(f"Error: {e}")


if __name__ == "__main__":
    main()
