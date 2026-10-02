namespace Even.Domain;

/// <summary>
/// Name of a <see cref="Group"/>, wrapped instead of a raw <see cref="string"/> (Object
/// Calisthenics: "wrap all primitives and strings") so the "cannot be empty" rule lives in one
/// place instead of being reimplemented at every edge that receives a group name.
/// </summary>
public readonly record struct GroupName
{
    public string Value { get; }

    private GroupName(string value)
    {
        Value = value;
    }

    public static GroupName Create(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new ArgumentException("Group name cannot be empty.", nameof(value));
        }

        return new GroupName(value.Trim());
    }

    public override string ToString() => Value;
}
