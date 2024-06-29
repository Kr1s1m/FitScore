
create table accounts(
    account_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    account_email character varying(255),
    account_username character varying(255),
    account_age smallint,
    account_height smallint,
    account_weight numeric(4,1)
);

insert into accounts(
	account_email,
	account_username,
	account_age,
	account_height,
	account_weight
) values (
	'dero@fitscore.com',
	'Dero',
	25,
	164,
	67.4
);

insert into accounts(
	account_email,
	account_username,
	account_age,
	account_height,
	account_weight
) values (
	'kr1s1m@fitscore.com',
	'Kr1s1m',
	25,
	173,
	54.5
);